#!/bin/bash

PRIVATE_KEY_PATH=""
PUBLIC_KEY_PATH=""
PASSWORD=""
KEY_NAME=""
KEY_STORE_PATH=""
SHELL_PATH=`pwd`

usage() {
    echo "The version of JDK must be 1.8.x"
    echo "usage:"
    echo "./rsaMac.sh rsaKeyGenerator --privateKeyPath /opt/private.der --publicKeyPath /opt/public.der"
    echo "./rsaMac.sh keyStoreGenerator --privateKeyPath /opt/private.der --publicKeyPath /opt/public.der --password abcd1234 --keyName fms --filePath /opt/testkeystore"
}

setRSAKeyEnv() {
    while :
    do
        case "$1" in
            --privateKeyPath)
                PRIVATE_KEY_PATH=$2;
                shift 2
                ;;
            --publicKeyPath)
                PUBLIC_KEY_PATH=$2;
                shift 2
                ;;
            *)
                break;
                ;;
        esac
    done
}

generateRSAKey() {
    if [[ -z $PRIVATE_KEY_PATH || -z $PUBLIC_KEY_PATH ]]; then
        usage
        exit 1
    fi
    export privateKeyPath=$PRIVATE_KEY_PATH
    export publicKeyPath=$PUBLIC_KEY_PATH

    echo `java -cp "$SHELL_PATH/crypto-standalone.jar" com.thoughtworks.fms.crypto.RSAKeyGenerator`
}

setKeyStoreEnv(){
    while :
    do
        case "$1" in
            --privateKeyPath)
                PRIVATE_KEY_PATH=$2;
                shift 2
                ;;
            --publicKeyPath)
                PUBLIC_KEY_PATH=$2;
                shift 2
                ;;
            --password)
                PASSWORD=$2;
                shift 2
                ;;
            --keyName)
                KEY_NAME=$2;
                shift 2
                ;;
            --filePath)
                KEY_STORE_PATH=$2;
                shift 2
                ;;
            *)
                break;
                ;;
        esac
    done
}

generateKeyStore() {
    if [[ -z $PRIVATE_KEY_PATH || -z $PUBLIC_KEY_PATH || -z $PASSWORD || -z $KEY_NAME || -z $KEY_STORE_PATH ]]; then
        usage
        exit 1
    fi

    export privateKeyPath=$PRIVATE_KEY_PATH
    export publicKeyPath=$PUBLIC_KEY_PATH
    export password=$PASSWORD
    export keyName=$KEY_NAME
    export filePath=$KEY_STORE_PATH

    echo `java -cp "$SHELL_PATH/crypto-standalone.jar" com.thoughtworks.fms.crypto.KeyStoreGenerator`
}

TEMP=`getopt -al privateKeyPath:,publicKeyPath:,password:,keyName:,filePath:,rsaKeyGenerator -- "$@"`

case "$1" in
    rsaKeyGenerator)
        eval set -- "${TEMP}"
        setRSAKeyEnv "$@"
        generateRSAKey
        ;;
    keyStoreGenerator)
        eval set -- "${TEMP}"
        setKeyStoreEnv "$@"
        generateKeyStore
        ;;
    *)
        usage
        exit 1
        ;;
esac