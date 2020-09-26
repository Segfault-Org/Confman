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

If you are using Confman, you will need a patch with a describing ini only:

```
$ cat ./config_package/etc/ssh/01-sshd_config
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

$ cat ./config_package/etc/ssh/01-sshd_config.ini
[Item]
Action = patch

ExecAfter = systemctl restart sshd

$ tree ./config_package/
config_package
└── etc
    └── ssh
        └── 01-sshd_config
        └── 01-sshd_config.ini
```

That's it. The folder `config_package` is your Confman package. You can create a NFS or Samba or whatever network share, so it is distributed over the network.

Now, execute:

```
$ sudo confman --dry-run ./config_package/
# Returns 0, means that there will be no error(s) in production execution

# Now the magic
$ sudo confman ./config_package/
```

## Docs

Refer to Wiki.

## TODO

* Testing

* Rewrite in C

# License

GPL v2 only.
