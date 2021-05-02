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
    use winapi::um::wincon::{FOREGROUND_RED, GetConsoleScreenBufferInfo, SetConsoleTextAttribute, CONSOLE_SCREEN_BUFFER_INFO, };
    use winapi::um::consoleapi::{GetConsoleMode, SetConsoleMode};
    use winapi::um::wincontypes::{COORD, SMALL_RECT};
    use std::os::windows::io::AsRawHandle;
    use std::io::stderr;

    let handle = stderr().as_raw_handle() as *mut winapi::ctypes::c_void;

    unsafe {
        let mut console_mode = 0;
        GetConsoleMode(handle, &mut console_mode);
        
        // ENABLE_VIRTUAL_TERMINAL_PROCESSING=0x0004
        // The reason I'm not using the constant is because IDK if it's defined in earlier windows version 
        if SetConsoleMode(handle, console_mode | 0x0004) == 0 {
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
            
            GetConsoleScreenBufferInfo(handle, &mut console_info);
            let saved_attributes = console_info.wAttributes;

            SetConsoleTextAttribute(handle, FOREGROUND_RED);
            eprint!("[error] ");
            SetConsoleTextAttribute(handle, saved_attributes);
        }
        else {
            eprint!("\x1b[31m[error]\x1b[0m ");
            SetConsoleMode(handle, console_mode);
        }
    }    

    eprintln!("{}", e);
}

#[cfg(unix)]
fn print_error(e: impl Display) {
    eprintln!("\x1b[31m[error]\x1b[0m {}", e);
}

#[cfg(not(any(windows, unix)))]
fn print_error(e: impl Display) {
    eprintln!("[error] {}", e);
}