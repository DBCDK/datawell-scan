# datawell-scan

A service that given a SolR (8.1+ in cloud mode) uses the terms handler to look
through a register and then fixes actual hitcount via a select request.

Input is agency/profile and term/register


It uses profile-service to get a filterquery, for the agency/profile and uses
it to limit the search to the records visible by the profile.

The register that the term handler is using is the one given as an argument with
a _{agency}_{profile}. Then the actual hitcount is checked against the real
register.

This allows for a register that covers only a profile, so that most (if not all)
the terms taken from the register are searchable by the profile. However this
requires that the updater knows about profiles, and adds them to the
solr-document
