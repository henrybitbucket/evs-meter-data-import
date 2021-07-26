#!/bin/sh
rm -rf awscli-bundle*
rm -rf awscli-bundle
rm -rf awscli-bundle/install
echo yes | curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
unzip -o awscli-bundle.zip
apt-get update
echo yes | apt-get install python
echo yes | apt-get install python3
#./awscli-bundle/install -b ~/bin/aws
./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws
/usr/local/aws/bin/aws configure set region ap-southeast-1
/usr/local/aws/bin/aws configure set aws_access_key_id AKIA5FJTG4T6ZDPEV2BF
/usr/local/aws/bin/aws configure set aws_secret_access_key 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
/usr/local/aws/bin/aws configure set output json

#/usr/local/aws/bin/aws acm-pca issue-certificate --certificate-authority-arn arn:aws:acm-pca:ap-southeast-1:842807477657:certificate-authority/e1f62f81-ddc1-4d1c-af40-bd612897c82f --csr file://gateway7.hdbsmarthome.com-eSE.csr --signing-algorithm "SHA256WITHECDSA" --validity Value=365,Type="DAYS"
#-->
#/usr/local/aws/bin/aws acm-pca get-certificate --certificate-authority-arn arn:aws:acm-pca:ap-southeast-1:842807477657:certificate-authority/e1f62f81-ddc1-4d1c-af40-bd612897c82f --certificate-arn "arn:aws:acm-pca:ap-southeast-1:842807477657:certificate-authority/e1f62f81-ddc1-4d1c-af40-bd612897c82f/certificate/359fa76e8ac7948240acc2801c6acc3c"

# device.pem
# echo -e $x | perl -0777 -lne 'print "$1\n" while m/.*"CertificateChain": \"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\/+]+-----END CERTIFICATE-----).*/gm' > device.pem

# sign_ca.pem
# echo -e $x | perl -0777 -lne 'print "$1\n" while m/.*-----END CERTIFICATE-----\n(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\/+]+-----END CERTIFICATE-----).*/gm' sign_ca.pem

# rootCA.crt
# echo -e $x | perl -0777 -lne 'print "$1\n" while m/.*"Certificate": \"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\/+]+-----END CERTIFICATE-----).*/gm' > rootCA.crt