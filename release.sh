cd PipeMusicClient
git pull
cd ../PipeMusicExtractor
git pull
cd ..
if [ -n "$1" ]; then
    vim fastlane/metadata/android/en-US/changelogs/$1.txt
else
    echo "No version number provided. Skipping changelog edit."
fi
git add .

git commit -a
git push origin HEAD
