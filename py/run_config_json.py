import json
from typing import Tuple, List

def create_run_config_json(num_runs: int, starting_seed: int, seed_multiplier: int, 
                           num_nodes: int, num_consensus: int, base_time_limit: float, 
                           node_processing_distribution: str, node_processing_parameters: List[float], 
                           consensus_protocol: str, num_faults: int, fault_type: str, fault_parameters: List[int],
                           switch_processing_distribution: str, switch_processing_parameters: List[float], 
                           message_channel_success_rate: float, 
                           network_type: str, network_parameters: list):
    switch_processing_distribution = {"distributionType": switch_processing_distribution, "parameters": switch_processing_parameters}
    switch_settings_dic = {"switchProcessingDistribution": switch_processing_distribution, "messageChannelSuccessRate": message_channel_success_rate}
    node_processing_distribution = {"distributionType": node_processing_distribution, "parameters": node_processing_parameters}
    network_settings_dic = {"switchSettings": switch_settings_dic, "networkType": network_type, "networkParameters": network_parameters}
    fault_settings_dic = {"numFaults": num_faults, "faultType": fault_type, "faultParameters": fault_parameters}
    validator_settings_dic = {"numNodes": num_nodes, "numConsensus": num_consensus, "baseTimeLimit": base_time_limit, 
                              "nodeProcessingDistribution": node_processing_distribution, 
                              "consensusProtocol": consensus_protocol, "faultSettings": fault_settings_dic}
    json_dic = {"numRuns": num_runs,"startingSeed": starting_seed, "seedMultiplier": seed_multiplier,
                "validatorSettings": validator_settings_dic, "networkSettings": network_settings_dic}
    return json.dumps(json_dic, indent=4)