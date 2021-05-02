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
    use winapi::um::wincon::{FOREGROUND_RED, GetConsoleScreenBufferInfo, SetConsoleTextAttribute, CONSOLE_SCREEN_BUFFER_INFO };
    use winapi::um::consoleapi::{GetConsoleMode, SetConsoleMode};
    use winapi::um::wincontypes::{COORD, SMALL_RECT};
    use winapi::shared::minwindef::DWORD;
    use std::os::windows::io::AsRawHandle;
    use std::io::{self, stderr};

    const ERROR_INVALID_PARAMETER: i32 = winapi::shared::winerror::ERROR_INVALID_PARAMETER as i32;
    // This can be found in winapi::um::wincon, but IDK if it's defined in older windows versions
    const ENABLE_VIRTUAL_TERMINAL_PROCESSING: DWORD = 0x0004;

    let handle = stderr().as_raw_handle() as *mut winapi::ctypes::c_void;

    unsafe {
        let mut console_mode = 0;
        if GetConsoleMode(handle, &mut console_mode) == 0 { // Not a tty
            eprintln!("[error] ");
        }
        else if SetConsoleMode(handle, console_mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING) == 0 { // ANSI not supported
            match io::Error::last_os_error().raw_os_error() {
                Some(ERROR_INVALID_PARAMETER) => {},
                Some(e) => {
                    eprintln!("Exited with status code {}", e);
                    std::process::exit(e);
                },
                None => unreachable!(),
            }
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
            
            try_sys(GetConsoleScreenBufferInfo(handle, &mut console_info));
            let saved_attributes = console_info.wAttributes;

            try_sys(SetConsoleTextAttribute(handle, FOREGROUND_RED));
            eprint!("[error] ");
            try_sys(SetConsoleTextAttribute(handle, saved_attributes));
        }
        else {
            eprint!("\x1b[31m[error]\x1b[0m ");
            SetConsoleMode(handle, console_mode);
        }
    }    

    eprintln!("{}", e);
}

#[cfg(windows)]
fn try_sys(r: i32) {
    use std::{io, process::exit};
    if r == 0 {
        match io::Error::last_os_error().raw_os_error() {
            Some(e) => {
                eprintln!("Exited with status code {}", e);
                exit(e);
            }
            None => {
                eprintln!("Error while making system call");
                exit(1);
            }
        }
    }
}

#[cfg(unix)]
fn print_error(e: impl Display) {
    if libc::isatty(std::io::stderr()) {
        eprint!("\x1b[31m[error]\x1b[0m ");
    }
    else {
        eprint!("[error] ")
    }
    println!("{}", e);
}

#[cfg(not(any(windows, unix)))]
fn print_error(e: impl Display) {
    eprintln!("[error] {}", e);
}