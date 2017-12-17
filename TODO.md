Various bits and pieces that I think might be useful to do, but have
not gotten around to doing.

* (Learn how to and) use webpack to package all the external
  libraries instead of the ad-hoc `site` scaffolding.

* Also use SASS to generate own CSS styles, based properly on
  bootstrap 4 sources...

* Move the final hard-coded dependencies from Main in UI to separate
  JSON resource files (this would make it much easier to retarget to
  other locations by just changing these)

* Add localization support and translate

* Add semantic versioning from repository version tags (and put that
  visible in the UI somewhere)

* Use diff in the URL base64, e.g. limit only to changes to the
  default model (but this would need also to encode what model it is
  based on in case of (see above) different sites, and/or version
  updates)

* Add SBT web server for local development.

* Clean up the publish script.
