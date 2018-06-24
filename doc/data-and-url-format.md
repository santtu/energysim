Map Data and URL Format
=======================

The map data and URL format are closely linked together. The map data
is best explained if you take a look at `site/data/world.json` file
first. This maps quite closely into the class
`fi.iki.santtu.energysim.model.World` and its fields and associated
types.

The web application will read this file and use it as the **default**
world definition. How things work from here on depends on the version
of URL handling as described below. (Note that both versions have the
same goal: keep the full world state in the URL so that it can shared
between users --- but see below.)

# Original version

This version keeps the current world state in the URL, as Base64
encoded JSON. So, this version will actually parse JSON in two
different places:

#. When reading the default world (`world.json`) --- this happens
   during the loading screen.
#. When parsing world data from the URL. This happens only if there is
   data to be parsed on the URL element, otherwise the default world
   is used.

While conceptually very simple, this has the problem that while this
worked nicely with Chrome during 2017, sometime during 2018 something
changed either in Chrome, in OS X or something that at least on my
machine imposed a maximum limit of URLs copied to the Chrome address
bar. **Of course** this limit is smaller than the Base64-encoded JSON
of the default world data.

Note that as of now this URL encoding format is supported as a
fall-back, so if you can cram a full world state into the URL of your
browser, then this should still work.

# Improved version

To decrease the amount of data to be stored in the URL, this version
takes advantage of the fact that all `id` values must be unique. Thus,
while it may not be known what **type** a particular identifier maps
to, it is guaranteed to be unique (not by the model, but by
construction of the world data). Also, the UI only allows specific
manipulations to occur (enable/disable sources and lines, manipulate
capacity of sources and lines) so we can actually encode only
**changes** made from the original model.

To differentiate between the original version and the improved
version, the URL of the "improved" version is prefixed with the string
`"2-"`. Since Base64 encoding does **not** use the minus (dash)
character, there is no ambiguity with the old format.

## Attempt 1 (this did not work)

This version continues encoding a Base64-encoded JSON data in the URL,
but instead of encoding the full world state it only encodes the
following type of data:

	{
		"name": <name from world.json>
		"version": <version from world.json>,
		"changes": [
			{
				"id": <id>,
				"enabled": <boolean>|null,
				"capacity": <int>|null
			},
			...
		]
	}

The `world` field contains information of the world model used, and
changes made to that version. The `name` and `version` are included to
allow graceful error message to be given to the user in case the URL
refers to changes made to a version of the world definition that can
not be used any longer. Finally, `changes` encodes all changes made to
the model where `enabled` is one of `true`, `false` or `null` and
`capacity` is either an integer or `null`. The `null` value for
enabled and capacity is taken as "no change from default".

To preserve space, only changes are encoded and thus `["...", null,
null]` is not possible encoding (it represents no change from the
default).

> However ... this turns out to still generate too long URLs. For
> example, disabling all sources (click on the "production capacity"
> checkbox on opening page) generates 8 kB of Base64-encoded JSON.

## Attempt 2

Following the previous idea of encoding changes, but use a binary
encoding instead. A few potential binary encoding formats are BSON
(binary JSON) and msgpack. Unfortunately for BSON, there are no useful
clean library implementations for Scala or ScalaJS. Msgpack has some
libraries, but has some general issues on documentation. So instead of
those, I decided to go for a packing library that does not conform to
existing specifications: BooPickle. Doing the same that resulted in 8
kB Base64-encoded URL part, with BooPickle the result is just 1.5 kB.

The `Changes` type is directly pickled with BooPickle into whatever
binary format it uses internally, then Base64-encoded (URL safe). This
results in URLs that at least with the Finnish model with all sources
and lines disabled results in URLs that can be copied and pasted.
