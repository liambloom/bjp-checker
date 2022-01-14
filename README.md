# Checker for BJP

This progeam is designed to check the code of exercises in programming books. The tests can be read from an XML file.

### Modules

This program is split into 3 modules:
- Annotations &mdash; This module only include the annotations used to annotate code so that the checker can find the exercises and programming projects in it. None of the logic is in this module.
- API &mdash; This module contains all the internal logic that loads, filters, and runs the tests and code to be tested. It also imports the annotations module.
- UI &mdash; This module contains all the code for the CLI and GUI, as well as importing the API module.
