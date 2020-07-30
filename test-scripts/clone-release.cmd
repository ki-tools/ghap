git config --global user.name "Vlad Ruzov"
git config --global user.email "vlad.ruzov@ontarget-group.com"
git config --global credential.helper "!aws --profile default codecommit credential-helper $@"
git config --global credential.UseHttpPath true
git clone --branch release https://git-codecommit.us-east-1.amazonaws.com/v1/repos/test-scripts
