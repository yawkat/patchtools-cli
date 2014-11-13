patchtools-cli
==============

Usage
-----

```
java -jar <> -i <input jar> -o <output jar> [--log-level OFF,SEVERE-FINEST,ALL] [--dependency <jar>]... <patch>...
```

Class Sets
----------

To increase speed and decrease likelihood of errors, class sets can be defined in patch files:

```
//exclude *
//include at.yawk.*

.class
  ...
```

The lower an include / exclude rule, the higher its priority.

If no rule matches a class, it is included by default (to maintain compatibility with patch files without these rules).