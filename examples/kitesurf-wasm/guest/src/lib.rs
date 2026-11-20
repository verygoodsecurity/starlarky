mod browser;
mod host;
mod page;

use browser::Browser;
use serde_json::{json, Value};
use std::cell::RefCell;
use std::io::{self, Read, Write};

thread_local! {
    static SESSION: RefCell<Option<Browser>> = const { RefCell::new(None) };
}

#[no_mangle]
pub extern "C" fn _initialize() {}

#[no_mangle]
pub extern "C" fn vgs_browser_step() {
    let result = read_command().and_then(execute);
    let reply = result.unwrap_or_else(|error| json!({"error": error}));
    let mut stdout = io::stdout().lock();
    let _ = stdout.write_all(reply.to_string().as_bytes());
    let _ = stdout.flush();
}

fn read_command() -> Result<Value, &'static str> {
    let mut input = Vec::new();
    io::stdin().take(16385).read_to_end(&mut input)
        .map_err(|_| "could not read the browser command")?;
    if input.len() > 16384 {
        return Err("browser command is too large");
    }
    serde_json::from_slice(&input).map_err(|_| "invalid browser command")
}

fn execute(command: Value) -> Result<Value, &'static str> {
    SESSION.with(|slot| {
        let mut session = slot.borrow_mut();
        let op = command.get("op").and_then(Value::as_str);
        if op == Some("open") {
            *session = None;
            let url = command.get("url").and_then(Value::as_str).ok_or("missing document URL")?;
            *session = Some(Browser::open(url)?);
            return Ok(json!({"opened": true}));
        }
        let browser = session.as_mut().ok_or("open a document first")?;
        match op {
            Some("inspect") => browser.inspect(),
            Some("match_text") => browser.match_text(&command),
            Some("fill") => browser.fill(&command),
            Some("click") => browser.click(&command),
            _ => Err("unknown browser command"),
        }
    })
}
