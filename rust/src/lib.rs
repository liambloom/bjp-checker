use std::{io, fmt, error::Error, process::Command, path::PathBuf, fs::write};
use std::env::{current_exe, args};
use chrono::Local;

#[derive(Debug, Copy, Clone)]
pub enum JRE {
    /// Calls the java command
    Java,

    /// Calls the javaw command
    JavaWindowed
}

impl JRE {
    // TODO: build for mac/linux https://stackoverflow.com/a/62853319/11326662
    pub fn run(&self, main: &str) -> Result<(), ErrorKind> {
        let mut here = install_location()?;
        here.push("lib");
        here.push("*");
        Command::new(self.to_string())
            .arg("-cp")
            .arg(here)
            .arg(main)
            .args(args().skip(1).map(|mut s| { s.push('\t'); s }))
            .spawn()
            .map_err(|e| ErrorKind::FailedToSpawn(e))?
            .wait()?;
        Ok(())
    }
}

impl fmt::Display for JRE {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        use JRE::*;

        match self {
            Java => write!(f, "java"),
            JavaWindowed => write!(f, "javaw"),
        }
    }
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

pub fn log(e: &impl Error) {
    if let Ok(mut here) = install_location() {
        here.push("logs");
        here.push(Local::now().format("%Y-%m-%d-%H-%M-%S.log").to_string());
        let _ = write(here, e.to_string());
    }
}

#[derive(Debug)]
pub enum ErrorKind {
    FailedToSpawn(io::Error),
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
            ErrorKind::FailedToSpawn(_) => write!(f, "Unable to start JVM. Make sure java is in your path."),
            ErrorKind::IoError(e) => write!(f, "{}", e),
        }
    }
}
