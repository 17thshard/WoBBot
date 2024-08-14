use crate::util::Result;
use html2md::parse_html;
use itertools::Itertools;
use reqwest::Url;
use serde::Deserialize;
use serenity::builder::CreateEmbed;

#[derive(Debug)]
pub struct Arcanum {
    base_url: String,
    icon: String,
}

#[derive(Clone, Eq, PartialEq, Debug, Deserialize)]
pub enum EventState {
    #[serde(rename = "APPROVED")]
    Approved,
    #[serde(rename = "PENDING")]
    Pending,
    #[serde(other)]
    Legacy,
}

#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct Entry {
    id: u64,
    event: u64,
    event_name: String,
    event_date: String,
    event_state: EventState,
    date: String,
    paraphrased: bool,
    modified_date: String,
    tags: Vec<String>,
    lines: Vec<Line>,
    note: Option<String>,
}

#[derive(Debug, Deserialize)]
#[allow(dead_code)]
pub struct PaginatedEntries {
    pub count: u64,
    next: Option<String>,
    previous: Option<String>,
    pub results: Vec<Entry>,
}

#[derive(Debug)]
pub struct PaginatedSearch<'a> {
    pub count: u64,
    pages: Vec<Option<PaginatedEntries>>,
    arcanum: &'a Arcanum,
    terms: Vec<String>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct Line {
    speaker: String,
    text: String,
}

impl Arcanum {
    async fn raw_request(&self, endpoint: &str, params: &[(&str, &str)]) -> Result<String> {
        let base_url = format!("{}{}", self.base_url, endpoint);
        let url = Url::parse_with_params(&base_url, params).expect("Invalid URL or parameters");

        let response = reqwest::get(url).await?.text().await?;
        Ok(response)
    }

    pub fn embed_entry(&self, entry: &Entry) -> CreateEmbed {
        // TODO: can be moved elsewhere if it is
        //       relevant for generating other messages
        const EMBED_COLOR: (u8, u8, u8) = (0, 99, 91);
        const TITLE_LIMIT: usize = 256;
        const FIELD_LIMIT: usize = 25;
        const FIELD_TEXT_LIMIT: usize = 1024;
        const ARCANUM_SUFFIX: &str = "*… (Check Arcanum for more.)*";

        fn pretty_print_date(date_str: &str) -> Option<String> {
            const MONTHS: [&str; 12] = [
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
            ];

            if let Some((Some(y), Some(m), Some(d))) = date_str
                .split('-')
                .map(|v| v.parse::<usize>().ok())
                .collect_tuple()
            {
                if m < MONTHS.len() {
                    return Some(format!("{} {d}, {y}", MONTHS[m]));
                }
            }

            None
        }

        fn truncate(s: &str, len: usize) -> &str {
            if let Some((idx, _)) = s.char_indices().nth(len) {
                &s[..idx]
            } else {
                s
            }
        }

        fn truncate_with_suffix(s: &str, len: usize, suffix: &str) -> String {
            if s.len() > len {
                truncate(s, len - suffix.len()).to_owned() + suffix
            } else {
                s.to_string()
            }
        }

        let flags: Vec<&str> = vec![
            (entry.event_state == EventState::Pending).then_some("__Pending Review__"),
            entry.paraphrased.then_some("__Paraphrased__"),
            (entry.event_state == EventState::Approved).then_some("__Approved__"),
        ]
        .into_iter()
        .flatten()
        .collect();

        // https://discord.com/developers/docs/resources/channel#embed-object-embed-limits

        let mut embed = CreateEmbed::new()
            .color(EMBED_COLOR)
            .url(format!(
                "{}/events/{}/#e{}",
                self.base_url, entry.event, entry.id
            ))
            .thumbnail(&self.icon);
        let mut left = 6000;

        let full_title = format!(
            "{} ({})",
            &entry.event_name,
            pretty_print_date(&entry.date).unwrap_or_else(|| entry.date.clone())
        );
        let title = truncate(&full_title, TITLE_LIMIT);
        embed = embed.title(title);
        left -= title.len();

        let description = if flags.is_empty() {
            "".into()
        } else {
            "**".to_owned() + &flags.join(" ") + "**"
        };
        embed = embed.description(&description);
        left -= description.len();

        for Line { speaker, text } in entry.lines.iter().take(FIELD_LIMIT) {
            let speaker = parse_html(speaker);
            let text = truncate_with_suffix(&parse_html(text), FIELD_TEXT_LIMIT, ARCANUM_SUFFIX);

            if speaker.len() + text.len() <= left {
                embed = embed.field(&speaker, &text, false);
                left -= speaker.len();
                left -= text.len();
            }
        }

        embed
    }

    pub async fn random_entry(&self) -> Result<Entry> {
        serde_json::from_str(&self.raw_request("/api/random_entry", &[]).await?).map_err(Into::into)
    }

    pub async fn search_entries(&self, terms: Vec<String>) -> Result<PaginatedSearch> {
        PaginatedSearch::new(self, terms).await
    }

    pub fn new(base_url: String, icon: String) -> Self {
        Arcanum { base_url, icon }
    }
}

impl<'a> PaginatedSearch<'a> {
    async fn new(arcanum: &'a Arcanum, terms: Vec<String>) -> Result<Self> {
        let first_page = Self::load_page(arcanum, &terms, 0).await?;
        let count = first_page.count;
        let num_pages = ((count as usize) + 1) / 250;
        let mut pages: Vec<_> = (0..num_pages).map(|_| None).collect();

        pages[0] = Some(first_page);

        Ok(Self {
            arcanum,
            terms,
            count,
            pages,
        })
    }

    pub async fn get_entry(&mut self, idx: usize) -> Result<Entry> {
        // TODO: There should be a way to get around the .clone() here
        let page_idx = idx / 250;
        let pagination_idx = idx - 250 * page_idx;

        if let Some(pagination) = &self.pages[page_idx] {
            Ok(pagination.results[pagination_idx].clone())
        } else {
            let page = Self::load_page(self.arcanum, &self.terms, page_idx).await?;
            let ret = page.results[pagination_idx].clone();

            self.pages[page_idx] = Some(page);

            Ok(ret)
        }
    }

    async fn load_page(
        arcanum: &'a Arcanum,
        terms: &[String],
        page: usize,
    ) -> Result<PaginatedEntries> {
        arcanum
            .raw_request(
                "/api/search_entry",
                &[
                    ("page_size", "250"),
                    ("ordering", "rank"),
                    ("page", &(page + 1).to_string()),
                    ("query", &terms.join("+")),
                ],
            )
            .await
            .and_then(|s| serde_json::from_str::<PaginatedEntries>(&s).map_err(Into::into))
    }
}
