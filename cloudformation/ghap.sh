#!/bin/sh

create=''
template_dir=''
erase=''
validate=''
driver_flag=''
stack=''

while getopts 'cud:evs:' flag; do
  case "${flag}" in
    c ) create='true' ;;
    u ) update='true' ;;
    v ) validate='true' ;;
    d ) template_dir="$OPTARG" ;;
    e ) erase='true' ;;
    s ) stack="$OPTARG" ;;
  esac
done

if [ $erase ]; then
  aws s3 rb s3://ghap-infrastructure --force
  exit
fi

if [ $create ]; then
  aws s3 mb s3://ghap-infrastructure
  driver_flag='-c'
fi

if [ $update ]; then
  driver_flag='-u'
fi

if [ $validate ]; then
  driver_flag='-v'
fi

aws s3 sync $template_dir s3://ghap-infrastructure/templates --acl private
./cf-driver.sh $driver_flag -s $stack -t \
https://s3.amazonaws.com/ghap-infrastructure/templates/ghap-formation.json \
-p "ParameterKey=KeyName,ParameterValue=ghap ParameterKey=SambaKeyName,ParameterValue=ghap-samba"

echo "Registering Bounce Topic"
aws ses set-identity-notification-topic --identity ghap.io --notification-type Bounce --sns-topic `aws sns list-topics | grep TopicArn | awk '{print $2}' | sed s/\"//g | grep -i bounce`

echo "Registering Complaint Topic"
aws ses set-identity-notification-topic --identity ghap.io --notification-type Complaint --sns-topic `aws sns list-topics | grep TopicArn | awk '{print $2}' | sed s/\"//g | grep -i complaint`
