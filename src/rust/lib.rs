use std::{io, fmt, error::Error, process::Command, path::PathBuf, fs::write};
use std::env::{self, current_exe, args};

// TODO: build for mac/linux https://stackoverflow.com/a/62853319/11326662
pub fn run(main: &str) -> Result<(), ErrorKind> {
    let mut here = install_location()?;
    here.push("lib");
    here.push("*");
    let mut args = args();
    args.next();
    let mut java = PathBuf::from(&env::var_os("JAVA_HOME").ok_or(ErrorKind::JavaHomeNotFound)?);
    java.push("bin");
    java.push("java");
    Command::new(java.as_os_str())
        .arg("-cp")
        .arg(here)
        .arg(main)
        .args(args)
        .spawn()?
        .wait()?;
    Ok(())
}

pub fn install_location() -> Result<PathBuf, ErrorKind> {
    let mut here = current_exe()?;
    while here.metadata()?.file_type().is_symlink() {
        here = here.read_link()?;
    }
    here.pop();
    here.pop();
    Ok(here)
}

pub fn log(e: impl Error) {
    if let Ok(mut here) = install_location() {
        here.push("logs");
        here.push("");
        let _ = write(here, e.to_string());
    }
}

#[derive(Debug)]
pub enum ErrorKind {
    JavaHomeNotFound,
    IoError(io::Error),
}

impl From<io::Error> for ErrorKind {
    fn from(err: io::Error) -> Self {
        Self::IoError(err)
    }
}

impl Error for ErrorKind {}

impl fmt::Display for ErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ErrorKind::JavaHomeNotFound => write!(f, "Environmental variable JAVA_HOME is not set"),
            ErrorKind::IoError(e) => write!(f, "{}", e),
        }
    }
}