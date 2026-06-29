# Altera Patches

<br>

A Morphe patch source for TikTok.

<br>

## Patches

<!-- PATCHES_START -->
> **[v1.0.0-dev.1](https://github.com/lyyako/altera-patches/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 TikTok&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 45.7.3 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Sanitize sharing links](#sanitize-sharing-links) | Removes tracking parameters from shared links. |  |
| [Settings](#settings) | Adds Altera settings to TikTok. |  |
| [Show seekbar](#show-seekbar) | Shows the native seekbar on videos where TikTok would normally hide it. |  |

</details>

<!-- PATCHES_END -->

<br>

## Add Source

On the device where Morphe is installed, open this link:

[Add Realme Link Patches for Morphe](https://morphe.software/add-source?github=lyyako/altera-patches)

Or paste the repo URL into Morphe's add source field manually:

`https://github.com/lyyako/altera-patches`

<br>

## Target

- **App:** TikTok (`com.zhiliaoapp.musically`)
- **Versions:** 45.7.3

<br>

## Build

```bash
./gradlew :patches:buildAndroid :patches:generatePatchesList
```

## License

This project reuses the GPLv3 licensing from the projects it was built on.

See [LICENSE](LICENSE) and [NOTICE](NOTICE).