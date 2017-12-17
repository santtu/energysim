Energy Simulator
================

To see this in action, please check out
to [energysim.kooma.net](http://energysim.kooma.net). For a bit more
academic background on this, please
see [this blog post](http://santtu.iki.fi/xxx).

# Building and testing

First you'll need to build the project. This will generate all of the
javascript that is required.

    sbt generateJs

Due to browser's loading policies you will need to start a static-file
web server in the `site` directory. I have not bothered setting this
up in SBT and have just used python3's in-built HTTP server module:

	cd site && python3 -m http.server 8000

After this go to `http://localhost:8000`.

You can also run the command line simulator through SBT like this
(also try `--help`):

    sbt libraryJVM/run -- <options> worldfile.yml

# Missing bits and pieces

See the `TODO.md` file. There are tons of things I have not gotten
around to do.

# LICENSE

Copyright 2017 Santeri Paavolainen

This work is licensed under Apache 2.0 License, see `LICENSE` file for
details.
