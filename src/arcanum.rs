use crate::util::Result;
use html2md::parse_html;
use itertools::Itertools;
use reqwest::Url;
use serde::Deserialize;
use serenity::builder::CreateEmbed;

pub struct Arcanum {
    base_url: String,
    icon: String,
}

#[derive(Eq, PartialEq, Debug, Deserialize)]
pub enum EventState {
    #[serde(rename = "APPROVED")]
    Approved,
    #[serde(rename = "PENDING")]
    Pending,
    #[serde(other)]
    Legacy,
}

#[derive(Debug, Deserialize)]
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
    note: String,
}

#[derive(Debug, Deserialize)]
pub struct PaginatedEntries {
    pub count: u64,
    next: Option<String>,
    previous: Option<String>,
    pub results: Vec<Entry>,
}

#[derive(Debug, Deserialize)]
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
        // some constants describing embed formatting
        // TODO: can be moved elsewhere if it is
        //       relevant for generating other messages
        const EMBED_COLOR: (u8, u8, u8) = (0, 99, 91);
        const TITLE_LIMIT: usize = 256;

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

        fn truncate(s: &String, len: usize) -> &str {
            if let Some((idx, _)) = s.char_indices().nth(len) {
                &s[..idx]
            } else {
                s
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

        // TODO: this should check for size, etc
        CreateEmbed::new()
            .title(truncate(
                &format!(
                    "{} ({})",
                    &entry.event_name,
                    pretty_print_date(&entry.date).unwrap_or_else(|| entry.date.clone())
                ),
                TITLE_LIMIT,
            ))
            .color(EMBED_COLOR)
            .url(format!(
                "{}/events/{}/#e{}",
                self.base_url, entry.event, entry.id
            ))
            .thumbnail(&self.icon)
            .fields(
                entry
                    .lines
                    .iter()
                    .map(|Line { speaker, text }| (parse_html(speaker), parse_html(text), false)),
            )
            .description(if flags.is_empty() {
                "".into()
            } else {
                "**".to_owned() + &flags.join(" ") + "**"
            })
    }

    pub async fn random_entry(&self) -> Result<Entry> {
        serde_json::from_str(&self.raw_request("/api/random_entry", &[]).await?).map_err(Into::into)
    }

    pub async fn search_entries(&self, terms: &[String]) -> Result<PaginatedEntries> {
        serde_json::from_str::<PaginatedEntries>(
            &self
                .raw_request(
                    "/api/search_entry",
                    &[
                        ("page_size", "250"),
                        ("ordering", "rank"),
                        ("query", &terms.join("+")),
                    ],
                )
                .await?,
        )
        .map_err(Into::into)
    }

    pub fn new(base_url: String, icon: String) -> Self {
        Arcanum { base_url, icon }
    }
}
