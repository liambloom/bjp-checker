use bjp_tests::*;
use std::fmt::Display;
use std::io::{stderr, Error as IoError};

fn main() {
    match run("dev.liambloom.tests.book.bjp.checker.CLI") {
        Ok(_) => {}
        Err(e @ ErrorKind::JavaHomeNotFound) => if let Err(e2) = print_error(e) { log(e2) },
        Err(ErrorKind::IoError(e)) => {
            print_error("An error was encountered internally. Check logs for more information");
            log(e);
        }
    }
}

#[cfg(windows)]
fn print_error(e: impl Display) -> Result<(), IoError> {
    use winapi::um::wincon::{FOREGROUND_RED, GetConsoleScreenBufferInfo, SetConsoleTextAttribute, CONSOLE_SCREEN_BUFFER_INFO, ENABLE_VIRTUAL_TERMINAL_PROCESSING };
    use winapi::um::consoleapi::{GetConsoleMode, SetConsoleMode};
    use winapi::um::wincontypes::{COORD, SMALL_RECT};
    use std::os::windows::io::AsRawHandle;

    const ERROR_INVALID_PARAMETER: i32 = winapi::shared::winerror::ERROR_INVALID_PARAMETER as i32;

    let handle = stderr().as_raw_handle() as *mut winapi::ctypes::c_void;

    unsafe {
        let mut console_mode = 0;
        if GetConsoleMode(handle, &mut console_mode) == 0 { // Not a tty
            eprintln!("[error] ");
        }
        else if SetConsoleMode(handle, console_mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING) == 0 { // ANSI not supported
            let err = IoError::last_os_error();
            match err.raw_os_error() {
                Some(ERROR_INVALID_PARAMETER) => {},
                Some(_) => return Err(err),
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
            
            try_sys(GetConsoleScreenBufferInfo(handle, &mut console_info))?;
            let saved_attributes = console_info.wAttributes;

            try_sys(SetConsoleTextAttribute(handle, FOREGROUND_RED))?;
            eprint!("[error] ");
            try_sys(SetConsoleTextAttribute(handle, saved_attributes))?;
        }
        else {
            eprint!("\x1b[31m[error]\x1b[0m ");
            try_sys(SetConsoleMode(handle, console_mode))?;
        }
    }    

    eprintln!("{}", e);

    Ok(())
}

#[cfg(windows)]
fn try_sys(r: i32) -> Result<(), IoError> {
    if r == 0 {
        Err(IoError::last_os_error())
    }
    else {
        Ok(())
    }
}

#[cfg(unix)]
fn print_error(e: impl Display) -> Result<(), IoError> {
    if libc::isatty(stderr()) {
        eprint!("\x1b[31m[error]\x1b[0m ");
    }
    else {
        eprint!("[error] ")
    }
    println!("{}", e);
    Ok(())
}

#[cfg(not(any(windows, unix)))]
fn print_error(e: impl Display) -> Result<(), IoError> {
    eprintln!("[error] {}", e);
    Ok(())
}