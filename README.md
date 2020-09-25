# Confman

Rapidly distribute Linux configurations within your network.

## What is Confman?

Confman is a tool that greatly simplifies your process of configuring lots of Linux machines, including desktop or servers.

Confman is not a package manager. The main goal of it is to replicate configurations, not software binaries. Thus, it does not include a centralized repository.

## How does it work?

The main idea behind Confman is that **Everything is a file**. On UNIX-like systems, most configuration are in plain text. Also, abstract files, like char devices, are also mapped to files. 

The basic thing Confman does is that it takes in a series of actions, based on files, and apply them to the target system.

## The purpose behind Confman

One of the biggest obstacles of deploying Linux desktop systems in enterprise is that GNU / Linux does not have a similar managing tool as Windows (Group Policy). Some existing managing tools are ready for servers. For example, Ansi*le, works pretty well for server, but not for desktop PCs, since it does not have a centralized configuration store.

Confman does not include the distribution of configurations, nor directory-integrated services like OU filtering or security group filtering. Instead, it is only a client software of applying them. How and when to distribute configuration in your network is your choice.

## Example
A typical workload would be to enable a setting in `sshd_config`. For example:

```
# /etc/ssh/sshd_config
...
# Original:
#Port 22

# Change it to:
Port 9022
```

You can quickly edit it by hand if there is only one or a few machines. However, if you have ten or more machines, it would be hard to manage. 

If you are using Confman, you will need a folder with the same name of the target system. Remember: everything is a file, and Confman configurations are just like overlays of the system.

```
$ mkdir config_change_port
$ cd config_change_port

$ mkdir -p etc/ssh
$ cd etc/ssh
```

Now, create a patch comparing the original `sshd_config` to the new `sshd_config`. An example would be:

```
--- Example
+++ ./sshd_config	2020-09-xx xx:xx:xx.046149134 -xxxx
@@ -10,7 +10,7 @@
 # possible, but leave them commented.  Uncommented options override the
 # default value.

-#Port 22
+Port 9022
 #AddressFamily any
 #ListenAddress 0.0.0.0
 #ListenAddress ::
```

put it to `config_change_port/etc/ssh`, naming it exactly as `01-sshd_config`, with no extensions (but with a leading order number). Remember, configuration in Confman is like overlays.

```shell
$ cd config_change_port
$ tree .
.
└── etc
    └── ssh
        └── 01-sshd_config
```

After changing the sshd config, you also need to restart the service. Confman can help you to do that via post-execution hooks. 

Now create a metadata file telling Confman what `sshd_config` is, how to apply patches, and what to do after that.

```
$ cat etc/ssh/01-sshd_config.ini
[Item]

# What action to do?
# Available currently: mkdir / install / delete / patch / exec
Action = patch

# Post execution hook
ExecAfter = systemctl restart sshd
```

That's it. The folder `config_change_port` is your Confman package. You can create a NFS or Samba or whatever network share, so it is distributed over the network.

Now, execute (without root!):

```
$ confman --dry-run ./config_change_port/
```

This tells Confman to perform a dry-run, which does the following:

* Checks whether `patch` utility exists in `$PATH`
* Checks whether `/etc/ssh/sshd_config` exists
* Checks whether we have write permission to `/etc/ssh/sshd_config`
* Checks whether `/etc/ssh/sshd_config` can apply your patch

If all of the conditions match, Confman will return zero, without output. Otherwise, detailed output is printed.

You will receive an error of telling you `/etc/ssh/sshd_config` cannot be written. Re-run Confman with root will solve this.

If you found everything is OK, remove `--dry-run` and run again with root, the new sshd configuration will be applied.

## Docs

After reading the example, you now have some basic concepts of how Confman works. For every file, you need an actual file with a `ini` file regarding to it.

Files wil be proceeded one by one, with your custom order. Each one is proceeded by an extension, defined in `Action` option. Currently only built-in actions are supported, but we are adding support to external actions.

If any item fails (can be either a failure in check, or a failure in hooks, or a failure during running), following items will not run.

Each item will run in the following order:

1. Check: Perform a dry-run, to check if there would be no problem doing actual changes.
2. Pre-exec hook: Do some preparation. This will make changes to your system.
3. Run: Actually run.
4. Post-exec hook: Do some cleanup. This will make changes to your system.

If any of them fails, the whole item is marked as fail, and following items will not run.

The full syntax of `ini` is the following:

```
[Item]
# Action of the config. 
Action = install / delete / mkdir / patch / exec

# Point to your custom config file. 
# In default, the file will be the same name as your ini (without ini extension)
# This is helpful when creating folders (set item to . to avoid creating additional files)
Item = .

# For all scripts below, %file will be replaced with the path to your file in package
# Your custom additional check script. Exit 0 if check passed.
ExecCheck = ./relate/path/to/your/custom/environment\ checking\ script.sh

# Your custom pre-exec script. Exit 0 to continue.
ExecBefore = ./will_be_ran_before_executing.sh %file 

# Your custom post-exec script. Exit 0 to indicate a success.
ExecAfter = ./will_be_ran_if_success.sh

# Only works if Action = install
# Intall: copy the file to the system.
[Install]
# Override the existing file.
Force = false

# Only works if Action = delete
[Delete]
# Do not report an error when checking if the file does not exist.
ContinueWhenNotExist = false

# Only works if Action = mkdir
[Mkdir]
# Does not report an error when checking if the folder exists.
Force = false

# Only works if Action = patch
[Patch]
# No options available

# Only works if Action = exec
[Exec]
# No options available
```

## TODO

* Testing

* Rewrite in C

* Support more builtin and external actions (extensions)

# License

GPL v2 only.