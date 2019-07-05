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


## Command line tools

There's a couple of commandline tools:

 * `datawell-scan-profile-change-monitor`
 * `datawell-scan-profile-change-update`

Both are jars that given no arguments of -h lists the usage.

 * the `monitor` jar checks a list of profiles taken from `profile-service` against
   a database cache, to see if any changes has occurred. If the 2 sets don't match
   an error is returned and the changes are listed on stdout.
 * the `update` jar finds tha changes between the database cache and `profile-service`
   selects the ids from the solr and queues then in solr-doc-store. then when that is
   done, the database is updated to reflect the current setup.
