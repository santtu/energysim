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

> Note: This version was deployed, and is considered version 2 of the
> URL encoding format (the original one is implicit version 1).

## Attempt 3

The previous version is already more compact, but still results in
over 1.5 kB of base64-encoded data to be appended to the URL. Most of
the space in the encoding is actually taken by identifiers --- the
identifiers of sources and lines in the version 1 of Finland take a
total of 1075 bytes. However, these identifiers should be stable in a
specific version of the world definition, so encoding the identifiers
as strings is a tautology. Taking advantage of this, it is possible to
squeeze the changes into a smaller blob.

Since all identifiers are unique, any stable ordering of them can be
indexed. E.g., instead of the string, it is possible to sort the
identifiers (of sources and lines) and store the index instead. We can
require that any changes into the world structure must be reflected in
the version number.

So, we could use a format such as `case class (name: String, version:
Int, changes: Seq[(Int, Option[Boolean], Option[Int])])`. Using
BooPickle, encoding `("world", 1,
List.fill(88)((0,Some(true),Some(1000))))` takes 537 bytes, and
expanded to base64 this will take 716 characters. Hooray! Right?

Well, we can encode this data directly as semi-readable string without
a binary encoding. If a single change is represented as
`<index><enabled><capacity>`, with comma as a separator. Here
`enabled` would be one of `+` (for enabled), `-` (for disabled) and
`=` (no change from default) and `capacity` just a straight
integer. So, a change of two sources could be represented as
`1+,2=1000`.

Wait, there's more! We actually do not need to encode the index at
all. It can be made implicit, e.g. the previous one would be `,+,1000,,,...`
(`=` can be omitted since there is no longer a risk of running index
into capacity).

So considering the earlier case of 88 repeats of explicitly enabled
sources and lines for capacity 1000. It would be encoded as 87 commas
plus 88 `+` characters and 88 times 4 digits for a total of 87 + 88 +
88 * 4 = 527 characters total.

This assumes that source and line capacities can be represented as
four digits. Although, it has to be said that doing the BooPickle from
above with capacity of 10000 results in 715 characters of base64
encoding, and the same with custm encoding results in 87+88+88*5=615
characters ... so, still more compact.

The downside of this is that there is the need for the
placeholders. This could be worked around if `<index>` was still
allowed, but omission of it was interpreted as implicit increment from
previous value. This would require an encoder with some heuristics on
when to use implicit indexing and when explicit indexing, though.

However, I am attracted by the apparent illegibility of
base64-encoding so that's what the version 3 is using.
