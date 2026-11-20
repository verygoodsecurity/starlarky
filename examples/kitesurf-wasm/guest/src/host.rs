use serde_json::Value;
use url::Url;

const MAX_RESOURCE_BYTES: usize = 1024 * 1024;

#[link(wasm_import_module = "vgs_browser_v1")]
extern "C" {
    fn http_get(url: *const u8, url_size: usize, output: *mut u8, capacity: usize) -> i32;
    fn http_post(
        url: *const u8,
        url_size: usize,
        input: *const u8,
        input_size: usize,
        output: *mut u8,
        capacity: usize,
    ) -> i32;
    fn wall_clock_ms(output: *mut i64) -> i32;
    fn aes_gcm_encrypt(
        key: *const u8,
        key_size: usize,
        iv: *const u8,
        iv_size: usize,
        aad: *const u8,
        aad_size: usize,
        tag_bits: i32,
        input: *const u8,
        input_size: usize,
        output: *mut u8,
        capacity: usize,
    ) -> i32;
    fn rsa_oaep_256_encrypt(
        n: *const u8,
        n_size: usize,
        e: *const u8,
        e_size: usize,
        input: *const u8,
        input_size: usize,
        output: *mut u8,
        capacity: usize,
    ) -> i32;
}

#[link(wasm_import_module = "wasi_snapshot_preview1")]
extern "C" {
    fn random_get(output: *mut u8, capacity: usize) -> i32;
}

pub fn fetch(url: &Url, body: Option<&str>) -> Result<String, &'static str> {
    let url = url.as_str().as_bytes();
    let mut result = vec![0; MAX_RESOURCE_BYTES];
    // These are guest-owned buffers, and the imports check both ranges before each exchange.
    let size = unsafe {
        match body {
            Some(body) => http_post(
                url.as_ptr(),
                url.len(),
                body.as_ptr(),
                body.len(),
                result.as_mut_ptr(),
                result.len(),
            ),
            None => http_get(url.as_ptr(), url.len(), result.as_mut_ptr(), result.len()),
        }
    };
    String::from_utf8(finish(result, size)?).map_err(|_| "resource is not UTF-8")
}

pub fn clock_ms() -> i64 {
    let mut output = 0;
    // The host import writes exactly one little-endian i64 to the given address.
    let status = unsafe { wall_clock_ms(&mut output) };
    if status == 0 {
        i64::from_le(output)
    } else {
        0
    }
}

pub fn random(count: usize) -> Result<Value, &'static str> {
    if count > 65536 {
        return Err("random request too large");
    }
    let mut output = vec![0; count];
    // The buffer remains writable for this synchronous WASI call.
    if count != 0 && unsafe { random_get(output.as_mut_ptr(), count) } != 0 {
        return Err("random source unavailable");
    }
    Ok(serde_json::json!(output))
}

pub fn encrypt_aes(args: &[Value]) -> Result<Value, &'static str> {
    let key = bytes(args.first(), 32)?;
    let iv = bytes(args.get(1), 128)?;
    let aad = bytes(args.get(2), MAX_RESOURCE_BYTES)?;
    let tag = args
        .get(3)
        .and_then(Value::as_i64)
        .and_then(|tag| i32::try_from(tag).ok())
        .ok_or("invalid tag length")?;
    let input = bytes(args.get(4), MAX_RESOURCE_BYTES)?;
    let mut output = vec![0; input.len() + 16];
    // All arrays come from the current guest realm and live through the synchronous import.
    let size = unsafe {
        aes_gcm_encrypt(
            key.as_ptr(),
            key.len(),
            iv.as_ptr(),
            iv.len(),
            aad.as_ptr(),
            aad.len(),
            tag,
            input.as_ptr(),
            input.len(),
            output.as_mut_ptr(),
            output.len(),
        )
    };
    Ok(serde_json::json!(finish(output, size)?))
}

pub fn encrypt_rsa(args: &[Value]) -> Result<Value, &'static str> {
    let n = bytes(args.first(), 512)?;
    let e = bytes(args.get(1), 8)?;
    let input = bytes(args.get(2), 512)?;
    let mut output = vec![0; n.len()];
    // The import takes only the public RSA key and an input buffer from this guest.
    let size = unsafe {
        rsa_oaep_256_encrypt(
            n.as_ptr(),
            n.len(),
            e.as_ptr(),
            e.len(),
            input.as_ptr(),
            input.len(),
            output.as_mut_ptr(),
            output.len(),
        )
    };
    Ok(serde_json::json!(finish(output, size)?))
}

fn bytes(value: Option<&Value>, max: usize) -> Result<Vec<u8>, &'static str> {
    let bytes = value
        .and_then(Value::as_array)
        .filter(|array| array.len() <= max)
        .ok_or("invalid byte array")?;
    bytes
        .iter()
        .map(|value| {
            value
                .as_u64()
                .and_then(|byte| u8::try_from(byte).ok())
                .ok_or("invalid byte")
        })
        .collect()
}

fn finish(mut bytes: Vec<u8>, size: i32) -> Result<Vec<u8>, &'static str> {
    let size = usize::try_from(size).map_err(|_| "browser host operation failed")?;
    if size > bytes.len() {
        return Err("invalid browser host result length");
    }
    bytes.truncate(size);
    Ok(bytes)
}
