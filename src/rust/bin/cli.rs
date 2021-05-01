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
    use winapi::um::wincon::{FOREGROUND_RED, GetConsoleScreenBufferInfo, SetConsoleTextAttribute, CONSOLE_SCREEN_BUFFER_INFO, CreateConsoleScreenBuffer, CONSOLE_TEXTMODE_BUFFER};
    use winapi::um::winnt::{GENERIC_READ, FILE_SHARE_READ, FILE_SHARE_WRITE};
    use winapi::um::processenv::SetStdHandle;
    use winapi::um::winbase::STD_ERROR_HANDLE;
    use winapi::um::wincontypes::{COORD, SMALL_RECT};
    use std::os::windows::io::{AsRawHandle, FromRawHandle};
    use std::io::{stdout, stderr, Write};
    use std::ptr::{null, null_mut};
    use std::default::Default;

    //stderr().as_raw_handle() as *mut winapi::ctypes::c_void
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

    println!("before");
    unsafe {
        // let handle = CreateConsoleScreenBuffer(GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE, null(), CONSOLE_TEXTMODE_BUFFER, null_mut());//winapi::um::processenv::GetStdHandle(winapi::um::winbase::STD_ERROR_HANDLE);
        // SetStdHandle(STD_ERROR_HANDLE, handle);
        eprintln!("test");

        if 0 == GetConsoleScreenBufferInfo(stderr().as_raw_handle() as *mut winapi::ctypes::c_void, &mut console_info as *mut CONSOLE_SCREEN_BUFFER_INFO) {
            println!("{}", winapi::um::errhandlingapi::GetLastError());
            println!("failed to get");
        }
        else {
            println!("get succeeded")
        }

        stderr().flush();

        eprintln!("uh-oh");

        let saved_attributes = console_info.wAttributes;
        if 0 == SetConsoleTextAttribute(stderr().as_raw_handle() as *mut winapi::ctypes::c_void, FOREGROUND_RED) {
            println!("{}", winapi::um::errhandlingapi::GetLastError());
            println!("failed to set");
        }
        else {
            println!("set succeeded")
        }
        eprint!("[error] ");
         SetConsoleTextAttribute(stderr().as_raw_handle() as *mut winapi::ctypes::c_void, saved_attributes);
        eprintln!("{}", e);
    }
    println!("after");
}

#[cfg(not(windows))]
fn print_error(e: impl Display) {
    eprintln!("\x1b[31m[error]\x1b[0m {}", e)
}