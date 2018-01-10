Energy Simulator
================

To see this in action, please check out
to [energysim.kooma.net](http://energysim.kooma.net). For a bit more
academic background on this, please
see [this blog post](http://santtu.iki.fi/2018/01/10/energy-simulator).

# Building and testing

First you'll need to build the project. This will generate all of the
javascript that is required.

    sbt generateJs

Due to browser's loading policies you will need to start a static-file
web server in the `site` directory. I have not bothered setting this
up in SBT and have just used python3's in-built HTTP server module:

	cd site; python3 -m http.server 8000

After this go to `http://localhost:8000`.

You can also run the command line simulator through SBT like this
(also try `--help`):

    sbt 'libraryJVM/run -r 1000 world.yml'

# Project structure

This is a ScalaJS project for a single-page application runnable in
modern browsers that implements a monte carlo simulation modeling the
electricity market (production, consumption, transmission, imports) of
Finland. The project is structured into four separate SBT projects:

* `library` that is a ScalaJS cross-project, meaning its targets are
  split into `libraryJVM` (JVM-targeted version) and `libraryJS`
  (JavaScript-targeted version). Sources for these are located in
  `library/shared` and `library/jvm` subdirectories (there is no
  JS-specific code in library).

* `worker`, a JS-only project that uses the simulator core from
  library and wraps it into a web worker. The source code for this is
  in `worker` subdirectory.

* `ui` that contains the actual user interface that runs in the
  browser, and code is in the `ui` subdirectory.

The UI component is not stand-alone, but requires some scaffolding
(HTML, CSS and external resources) that is in the `site` subdirectory.

# Missing bits and pieces

See the `TODO.md` file. There are tons of things I have not gotten
around to do.

# LICENSE

Copyright 2017 Santeri Paavolainen.

This work is licensed under Apache 2.0 License, see `LICENSE` file for
details.
