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
  aws s3 rb s3://ghap-samba-infrastructure --force
  exit
fi

if [ $create ]; then
  aws s3 mb s3://ghap-samba-infrastructure
  driver_flag='-c'
fi

if [ $update ]; then
  driver_flag='-u'
fi

if [ $validate ]; then
  driver_flag='-v'
fi

aws s3 sync $template_dir s3://ghap-samba-infrastructure/templates --acl private
./cf-driver.sh $driver_flag -s $stack -t \
https://s3.amazonaws.com/ghap-samba-infrastructure/templates/ghap-samba-formation.json \
-p "ParameterKey=KeyName,ParameterValue=ghap-dev ParameterKey=SambaKeyName,ParameterValue=ghap-samba"
