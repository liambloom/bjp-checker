use std::process::Command;
use std::env::{self, current_exe, args, VarError};
use std::ffi::OsStr;

// FIXME: unwrap() BAD
// TODO: Follow symlinks from current_exe()
// TODO: build for mac/linux https://stackoverflow.com/a/62853319/11326662
pub fn run(main: &str) {
    let mut here = current_exe().unwrap();
    here.pop();
    here.pop();
    here.push("lib");
    here.push("*");
    let mut args = args();
    args.next();
    let java_home_var = env::var("JAVA_HOME");
    let java_home: OsStr = match java_home_var {
        Ok(s) => s.as_ref(),
        Err(VarError::NotUnicode(s)) => s.as_os_str(),
        Err(VarError::NotFound) => {

        }
    };
    Command::new(java_home + "java")
        .arg("-cp")
        .arg(here)
        .arg(main)
        .args(args)
        .spawn().unwrap()
        .wait()
        .unwrap();
}