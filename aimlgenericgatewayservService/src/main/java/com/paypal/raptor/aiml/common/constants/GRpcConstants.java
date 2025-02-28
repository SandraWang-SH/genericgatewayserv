package com.paypal.raptor.aiml.common.constants;

public class GRpcConstants {

    public enum ClusterName {
        HRZ, GKE, OPENSHIFT
    }

    public static final String GRPC_HEADER_NAMESPACE = "namespace";

    public static final String GRPC_HEADER_SELDON = "seldon";

    public static final String V2_MODEL_NAME = "v2_model_name";

    public static final String HRZ_GRPC_HOST = "aiplatform-grpc.ccg15-hrz-gke-generic.ccg15.slc.paypalinc.com";

    public static final String HRZ_ANA_GRPC_HOST = "aiplatform-grpc.ccg24-hrzana-edk8s.ccg24.lvs.paypalinc.com";

    public static final String GKE_DVE_GRPC_HOST = "aiplatform-grpc.dev51.cbf.dev.paypalinc.com";

}
