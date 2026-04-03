# Supabase CLI Install Guide (Windows + macOS)

Use this guide to install Supabase CLI for local development.

## Windows

Primary walkthrough (community guide):

- [How to install Supabase CLI on Windows (Dev.to)](https://dev.to/chiragx309/how-to-install-supabase-cli-on-windows-the-right-way-a-simple-guide-for-everyone-14om)

Install Scoop first (if you do not have it yet):

```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
iwr -useb get.scoop.sh | iex
```

Install Supabase CLI with Scoop:

```powershell
scoop install supabase
```

Verify:

```powershell
supabase --version
```

Reference:

- [Scoop](https://scoop.sh/)

## macOS (Homebrew)

Install Homebrew (if needed):

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Install Supabase CLI:

```bash
brew install supabase/tap/supabase
```

Verify:

```bash
supabase --version
```

Reference:

- [Homebrew](https://brew.sh/)
- [Supabase CLI Docs](https://supabase.com/docs/guides/cli)
