use std::process::Command;
use std::env::{current_exe, args, var};

// FIXME: unwrap() BAD
// TODO: Follow symlinks from current_exe()
// TODO: build for mac/linux https://stackoverflow.com/a/62853319/11326662
fn main() {
    let one_str = String::from("1");
    if let Ok(one_str) = var("CHECKER_DEBUG") {
        println!("Debug mode is ON");
    }
    let mut here = current_exe().unwrap();
    here.pop();
    here.pop();
    here.push("lib");
    here.push("*");
    Command::new("java")
        .arg("-cp")
        .arg(here)
        .arg("dev.liambloom.tests.book.bjp3.App")
        .args(&args().collect::<Vec<String>>()[1..])
        .spawn().unwrap()
        .wait();
}