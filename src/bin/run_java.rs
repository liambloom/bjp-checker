use std::process::Command;
use std::env::{current_exe, args};

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
    Command::new("java")
        .arg("-cp")
        .arg(here)
        .arg(main)
        .args(args)
        .spawn().unwrap()
        .wait()
        .unwrap();
}