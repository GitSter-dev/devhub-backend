# Releases and client versions

Every merge to `main` is deployed, so production always runs a commit SHA. A
**release** gives that build a name. The GitHub release lists what changed, and
the image gets a version tag you can roll back to. Nothing is rebuilt.

## Cutting a release

1. Merge to `main` and wait for the deploy to go green.
2. Actions → **release** → Run workflow on `main`, with the version (`1.2.0`, no `v`).

The workflow tags the image from the last successful deploy of `main` as `v1.2.0`
in ECR, then creates the `v1.2.0` git tag and a GitHub release with generated notes
on the same commit. ECR tags are immutable, so a version always means the same build.

If you rolled back by hand since the last deploy of `main`, production is not
running `main`'s build. Deploy `main` again before releasing.

### Choosing the number

| Change | Bump |
|---|---|
| Bug fixes, internal changes | PATCH: `1.2.0` → `1.2.1` |
| New endpoints or fields that old apps can ignore | MINOR: `1.2.1` → `1.3.0` |
| A change old apps can't handle | MAJOR, and ship it as a new `/v2` path next to `/v1` |

The API never breaks in place. Apps in the field don't update themselves.

## Rolling back

Actions → **deploy** → Run workflow, with `image_tag` set to a release (`v1.1.0`)
or a commit SHA.

## Retiring old app versions

The app sends `X-App-Platform` and `X-App-Version` on every request. When an app is
older than the minimum for its platform, the backend answers `426` with error code
`APP_UPDATE_REQUIRED`, and the app shows a screen linking to the latest APK.

The minimums default to `0.0.0` (gate off). To raise one:

```bash
aws ssm put-parameter --region eu-north-1 --type String --overwrite \
  --name /devhub/prod/MIN_ANDROID_VERSION --value 1.1.0
```

Then re-run the deploy with the current `image_tag` so the instance re-renders its
environment. Before raising a minimum, make sure the newer APK is published and
works: everyone below it is locked out until they update.

Builds from before the headers existed (Android `1.0.0`) send nothing and can't be
gated.
