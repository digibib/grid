#!/bin/bash -x

GU_DIR=/etc/gu
ASW_DIR=$HOME/.aws
mkdir -p ${GU_DIR}
mkdir -p $HOME/.gu/grid
mkdir -p ${AWS_DIR}

cat <<EOF | tee ${GU_DIR}/auth.properties
domain.root=${DOMAIN_ROOT}
cors.allowed.origins=*
s3.config.bucket=configbucket
auth.keystore.bucket=keybucket
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
EOF

cat <<EOF | tee ${GU_DIR}/collections.properties
domain.root=${DOMAIN_ROOT}
auth.keystore.bucket=keybucket
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
s3.collections.bucket=collectionsbucket
dynamo.table.collections=CollectionsDynamoTable
dynamo.table.imageCollections=ImageCollectionsDynamoTable
sns.topic.arn={{SnsTopicArn}}
EOF

cat <<EOF | tee ${GU_DIR}/cropper.properties
domain.root=${DOMAIN_ROOT}
auth.keystore.bucket=keybucket
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
s3.config.bucket=configbucket
publishing.image.bucket=imageoriginbucket
publishing.image.host={{ImageOriginWebsite}}
publishing.aws.id=${AWS_ACCESS_KEY_ID}
publishing.aws.secret=${AWS_SECRET_ACCESS_KEY}
sns.topic.arn={{SnsTopicArn}}
EOF

cat <<EOF | tee ${GU_DIR}/ftp-watcher.properties
ftp.active=true
ftp.host=localhost
ftp.port=41756
ftp.user=any
ftp.password=any
loader.uri=https://loader.{{domain_root}}/images
auth.key.ftpwatcher={{ftp_key}}
metrics.aws.id=${AWS_ACCESS_KEY_ID}
metrics.aws.secret=${AWS_SECRET_ACCESS_KEY}
EOF

cat <<EOF | tee ${GU_DIR}/image-loader.properties
domain.root=${DOMAIN_ROOT}
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
s3.image.bucket=imagebucket
s3.thumb.bucket=thumbbucket
auth.keystore.bucket=keybucket
sns.topic.arn={{SnsTopicArn}}
EOF

cat <<EOF | tee ${GU_DIR}/kahuna.properties
domain.root=${DOMAIN_ROOT}
aws.region=${AWS_REGION}
auth.keystore.bucket=keybucket
origin.full=minio:9000/imagebucket
origin.thumb=minio:9000/thumbbucket
origin.images=minio:9000/imagebucket
origin.crops=minio:9000/imagebucket
EOF

cat <<EOF | tee ${GU_DIR}/leases.properties
domain.root=${DOMAIN_ROOT}
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
auth.keystore.bucket=keybucket
sns.topic.arn={{SnsTopicArn}}
dynamo.tablename.leasesTable=LeasesDynamoTable
EOF

cat <<EOF | tee ${GU_DIR}/media-api.properties
domain.root=${DOMAIN_ROOT}
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
s3.image.bucket=imagebucket
s3.thumb.bucket=thumbbucket
auth.keystore.bucket=keybucket
sns.topic.arn={{SnsTopicArn}}
cors.allowed.origins=*
s3.config.bucket=configbucket
persistence.identifier=picdarUrn
mixpanel.token={{mixpanel_token}}
es.host=elasticsearch
es.index.aliases.read=readAlias
EOF

cat <<EOF | tee ${GU_DIR}/metadata-editor.properties
domain.root=${DOMAIN_ROOT}
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
auth.keystore.bucket=keybucket
s3.collections.bucket=collectionsbucket
sns.topic.arn={{SnsTopicArn}}
dynamo.table.edits=EditsDynamoTable
indexed.images.sqs.queue.url={{IndexedImageMetadataQueueUrl}}
EOF

cat <<EOF | tee ${GU_DIR}/panda.properties
panda.domain=${DOMAIN_ROOT}
panda.aws.key=${AWS_ACCESS_KEY_ID}
panda.aws.secret=${AWS_SECRET_ACCESS_KEY}
EOF

cat <<EOF | tee ${GU_DIR}/s3watcher.properties
aws.region=${AWS_REGION}
loader.uri=https://loader.{{domain_root}}
auth.key.s3watcher={{s3watcher_key}}
s3.ingest.bucket=S3WatcherIngestBucket
s3.fail.bucket=S3WatcherFailBucket
EOF

cat <<EOF | tee ${GU_DIR}/thrall.properties
domain.root=${DOMAIN_ROOT}
aws.id=${AWS_ACCESS_KEY_ID}
aws.secret=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION}
s3.image.bucket=imagebucket
s3.thumb.bucket=thumbbucket
sqs.queue.url={{SqsQueueUrl}}
sqs.message.min.frequency={{sqs_message_min_frequency}}
persistence.identifier=picdarUrn
es.host=elasticsearch
es.index.aliases.write=writeAlias
es.index.aliases.read=readAlias
indexed.image.sns.topic.arn={{IndexedImageTopicArn}}
EOF

cat <<EOF | tee ${GU_DIR}/grid-prod.properties
EOF

cat <<EOF | tee $HOME/.gu/grid/grid-settings.yml
# awscli profile name, defaulted to media-service Hint cat ~/.aws/credentials
aws_profile: media-service
properties:
  - domain_root: deichman.no
  # Token used to access mixpanel. See https://mixpanel.com/help/questions/articles/where-can-i-find-my-project-token
  - mixpanel_token:
  # Comma separated list of CORS domains.
  - cors: *

  # Configuration for pan domain authentication. See https://github.com/guardian/pan-domain-authentication
  - panda_domain:

  # Configuration for pan domain authentication. See https://github.com/guardian/pan-domain-authentication
  - panda_aws_key:

  # Configuration for pan domain authentication. See https://github.com/guardian/pan-domain-authentication
  - panda_aws_secret:
  - sqs_message_min_frequency: 5

  # An API key used to ingest images from the ftp-loader.
  - ftp_key:

  # An API key used to ingest images from the s3watcher.
  - s3watcher_key:
EOF

cat <<EOF | tee ${GU_DIR}/stage
DEV
EOF

cat <<EOF | tee ${AWS_DIR}/config
[default]
region=deichman
EOF

cat <<EOF | tee ${AWS_DIR}/credentials
[default]
aws_access_key_id=${AWS_ACCESS_KEY_ID}
aws_secret_access_key=${AWS_SECRET_ACCESS_KEY}
EOF
