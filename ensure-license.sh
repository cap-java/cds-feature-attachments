# Requires bash in version > 4.0.0
shopt -s globstar

year=$(date +"%Y")
license_indicator="SAP SE or an SAP affiliate company. All rights reserved."
license1="**************************************************************************"
license2=" * (C) 2019-$year $license_indicator *"
license3=" **************************************************************************"
java_files=$(printf %s\\n */src/main/java/**/*.java)

write_license () {
  if [[ "$OSTYPE" == "darwin"* ]]; then
     sed -i '' "1i\\
/$license1\\
$license2\\
$license3/\\
" $file
  else
    sed -i "1i/$license1\n$license2\n$license3/" $1
  fi
}

update_license () {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "1s/.*/\/$license1/" $1
    sed -i '' "2s/.*/$license2/" $1
    sed -i '' "3s/.*/$license3\//" $1
  else
    sed -i "1s/.*/\/$license1/" $1
    sed -i "2s/.*/$license2/" $1
    sed -i "3s/.*/$license3\//" $1
  fi
}

while read -r file; do
  if ! grep -q -F "$license_indicator" "$file"; then
    echo "Writing license to '$file'"
    write_license $file
  else
    echo "Updating license in '$file'"
    update_license $file
  fi
done <<< "$java_files"