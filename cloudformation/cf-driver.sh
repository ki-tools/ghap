#!/bin/bash

create=''
update=''
mode=''
stack_name=''
template=''
params=''
validate=''

while getopts 'cus:t:p:v' flag; do
  case "${flag}" in
    c ) create='true' ;;
    u ) update='true' ;;
    s ) stack_name="$OPTARG" ;;
    t ) template="$OPTARG" ;;
    p ) params="$OPTARG" ;;
    v ) validate='true' ;;
  esac
done

if [ $create ]; then
  mode='create-stack'
fi

if [ $update ]; then
  mode='update-stack'
fi

if [ $validate ]; then
  mode='validate-template'
fi

if [ $validate ]; then
    aws cloudformation $mode --template-url $template
else
  if [ ! -z "$params" ]; then
    aws cloudformation $mode --stack-name $stack_name --template-url $template \
    --capabilities CAPABILITY_IAM --parameters $params 
  else
    aws cloudformation $mode --stack-name $stack_name --template-url $template \
    --capabilities CAPABILITY_IAM
  fi
fi

