package software.amazon.rds.globalcluster;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<RdsClient> proxyClient,
                                                                          final Logger logger) {
        ResourceModel model = request.getDesiredResourceState();

        ProgressEvent<ResourceModel, CallbackContext> result = ProgressEvent.progress(model, callbackContext)
                .then(progress -> removeFromGlobalCluster(proxy, proxyClient, progress))
                .then(progress -> waitForDBClusterAvailableStatus(proxy, proxyClient, progress))
                .then(progress -> proxy.initiate("rds::delete-global-cluster", proxyClient, request.getDesiredResourceState(), callbackContext)
                        .translateToServiceRequest(Translator::deleteGlobalClusterRequest)
                        .backoffDelay(BACKOFF_STRATEGY)
                        .makeServiceCall((deleteGlobalClusterRequest, proxyInvocation)
                                -> proxyInvocation.injectCredentialsAndInvokeV2(deleteGlobalClusterRequest, proxyInvocation.client()::deleteGlobalCluster))
                        // wait until deleted
                        .stabilize(((deleteGlobalClusterRequest, deleteGlobalClusterResponse, stabilizeProxy, stabilizeModel, context)
                                -> isDeleted(stabilizeModel, stabilizeProxy)))
                        .success());

        result.setResourceModel(null);

        return result;
    }
}