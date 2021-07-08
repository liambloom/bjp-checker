use bjp_tests::*;
use native_dialog::{MessageDialog, MessageType};

fn main() {
    match JRE::JavaWindowed.run("dev.liambloom.tests.book.bjp.checker.GUIWrapper") {
        Ok(_) => {}
        Err(e @ ErrorKind::FailedToSpawn(_)) => {
            if let Err(e) = MessageDialog::new()
                .set_title("Application Error")
                .set_text(e.to_string().as_str())
                .set_type(MessageType::Error)
                .show_alert()
            {
                log(&e);
            }
        }
        Err(ErrorKind::IoError(e)) => {
            log(&e);
            if let Err(e) = MessageDialog::new()
                .set_title("Internal application error")
                .set_text("An error was encountered internally. Check logs for more information")
                .set_type(MessageType::Error)
                .show_alert()
            {
                log(&e);
            }
        }
    }
}