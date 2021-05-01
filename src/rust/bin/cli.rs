use bjp_tests::*;
use std::fmt::Display;

fn main() {
    match run("dev.liambloom.tests.book.bjp.checker.CLI") {
        Ok(_) => {}
        Err(e @ ErrorKind::JavaHomeNotFound) => print_error(e),
        Err(ErrorKind::IoError(e)) => {
            print_error("An error was encountered internally. Check logs for more information");
            log(e);
        }
    }
}

#[cfg(windows)]
fn print_error(e: impl Display) {
    use winapi::um::wincon::{FOREGROUND_RED, GetConsoleScreenBufferInfo, SetConsoleTextAttribute, CONSOLE_SCREEN_BUFFER_INFO};
    use winapi::um::wincontypes::{COORD, SMALL_RECT};
    use winapi::um::errhandlingapi::GetLastError;
    use std::os::windows::io::AsRawHandle;
    use std::io::stderr;
    use std::process::exit;

    let mut console_info = CONSOLE_SCREEN_BUFFER_INFO {
        dwSize: COORD { X: 0, Y: 0 },
        dwCursorPosition: COORD { X: 0, Y: 0 },
        wAttributes: 0,
        srWindow: SMALL_RECT {
            Left: 0,
            Top: 0,
            Right: 0,
            Bottom: 0
        },
        dwMaximumWindowSize: COORD { X: 0, Y: 0 }
    };

    let handle = stderr().as_raw_handle() as *mut winapi::ctypes::c_void;

    unsafe {
        if GetConsoleScreenBufferInfo(handle, &mut console_info) == 0 { exit(GetLastError() as i32); }
        let saved_attributes = console_info.wAttributes;

        if SetConsoleTextAttribute(handle, FOREGROUND_RED) == 0 { exit(GetLastError() as i32); }
        eprint!("[error] ");
        if SetConsoleTextAttribute(handle, saved_attributes) == 0 { exit(GetLastError() as i32); }
    }

    eprintln!("{}", e);
}

#[cfg(unix)]
fn print_error(e: impl Display) {
    eprintln!("\x1b[31m[error]\x1b[0m {}", e);
}

#[cfg(all(not(windows), not(unix)))]
fn print_error(e: impl Display) {
    eprintln!("[error] {}", e);
}