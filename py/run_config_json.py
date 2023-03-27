import json


def create_run_config_json(num_runs: int, num_consensus: int, starting_seed: int,
                           seed_multiplier: int, num_nodes: int, node_processing_rate: float,
                           switch_processing_rate: float, base_time_limit: float, network_type: str,
                           network_parameters: list):
    json_dic = {"numRuns": num_runs, "numConsensus": num_consensus, "startingSeed": starting_seed,
                "seedMultiplier": seed_multiplier, "numNodes": num_nodes, "nodeProcessingRate": node_processing_rate,
                "switchProcessingRate": switch_processing_rate, "baseTimeLimit": base_time_limit,
                "networkType": network_type, "networkParameters": network_parameters}
    return json.dumps(json_dic, indent=4)