use crate::util::Result;
use html2md::parse_html;
use reqwest::Url;
use serde::Deserialize;
use serenity::builder::CreateEmbed;

pub struct Arcanum {
    base_url: String,
    icon: String,
}

#[derive(Debug, Deserialize)]
pub struct Entry {
    id: u64,
    event: u64,
    event_name: String,
    event_date: String,
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
        // TODO: this should check for size, etc
        CreateEmbed::new()
            .title(&entry.event_name)
            .color((0, 99, 91))
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
    }

    pub async fn random_entry(&self) -> Result<Entry> {
        serde_json::from_str(&self.raw_request("/api/random_entry", &[]).await?).map_err(Into::into)
    }

    pub async fn search_entries(&self, terms: &[String]) -> Result<PaginatedEntries> {
        serde_json::from_str::<PaginatedEntries>(
            &self
                .raw_request(
                    "/api/search_entry",
                    &vec![
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
