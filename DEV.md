
```sh

# set to release version -> git tag the commit -> push/release
./mill versionFile.setReleaseVersion
./mill mill.contrib.versionfile.VersionFileModule/exec --procs versionFile.tag
./mill mill.contrib.versionfile.VersionFileModule/exec --procs versionFile.push



# or prepare for next major version
./mill versionFile.setNextVersion --bump major
# prepare for next minor version
./mill versionFile.setNextVersion --bump minor
# prepare for next patch version
./mill versionFile.setNextVersion --bump patch


$VERSION="0.1.0"
git tag -a $VERSION -m "Release $VERSION"
git push origin main $VERSION

```