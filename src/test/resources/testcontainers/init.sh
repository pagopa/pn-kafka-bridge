echo "### CREATE QUEUES ###"
queues="local-onboarding-institutes local-kafka-bridge-payments-DLQ local-kafka-bridge-payments local-payments-to-verify"
for qn in  $( echo $queues | tr " " "\n" ) ; do
    echo creating queue $qn ...
    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
done

echo "Initialization terminated"