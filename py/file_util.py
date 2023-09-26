import json
from typing import List
import re
import os

class ResultName:

    def set_num_nodes(self, num_nodes: int):
        self.num_nodes = num_nodes
        return self

    def get_num_nodes(self):
        return self.num_nodes

    def set_btl(self, btl: float):
        self.btl = btl
        return self

    def get_btl(self):
        return self.btl
    
    def set_node_dist(self, node_dist: str):
        self.node_dist = node_dist
        return self

    def get_node_dist(self):
        return self.node_dist
    
    def set_node_params(self, node_params: List[float]):
        self.node_params = node_params
        return self

    def get_node_params(self):
        return self.node_params

    def set_topology(self, topology: str):
        self.topology = topology
        return self
    
    def get_topology(self):
        return self.topology

    def set_topo_params(self, topo_params: List[int]):
        self.topo_params = topo_params
        return self

    def get_topo_params(self):
        return self.topo_params

    def set_switch_dist(self, switch_dist: str):
        self.switch_dist = switch_dist
        return self

    def get_switch_dist(self):
        return self.switch_dist

    def set_switch_params(self, switch_params: List[float]):
        self.switch_params = switch_params
        return self

    def get_switch_params(self):
        return self.switch_params

    def set_protocol(self, protocol: str):
        self.protocol = protocol
        return self

    def get_protocol(self):
        return self.protocol
    
    def set_num_faults(self, num_faults: int):
        self.num_faults = num_faults
        return self

    def get_num_faults(self):
        return self.num_faults

    def set_fault_type(self, fault_type: str):
        self.fault_type = fault_type
        return self

    def get_fault_type(self):
        return self.fault_type

    def set_fault_params(self, fault_params: List[int]):
        self.fault_params = fault_params
        return self
    
    def get_fault_params(self):
        return self.fault_params

    def build(self):
        return RESULTS_DIRECTORY.format(num_nodes=self.num_nodes, base_time_limit=self.btl, 
                                    node_dist=self.node_dist, node_params=self.node_params, 
                                    topology=self.topology, topo_params=self.topo_params, switch_dist=self.switch_dist, switch_params=self.switch_params, 
                                    protocol=self.protocol, num_faults=self.num_faults, fault_type=self.fault_type, fault_params=self.fault_params)

RESULTS_DIRECTORY = "json_n{num_nodes}_btl{base_time_limit:.1f}_{node_dist}_{node_params}_{topology}_{topo_params}_{switch_dist}_{switch_params}" +  \
                    "_{protocol}_{num_faults}_{fault_type}_{fault_params}"

RESULTS_FOLDER_REGEX = r'json_n(.+)_btl(.+)_(.+)_(.+)_(.+)_(.+)_(.+)_(.+)_(.+)_(.+)_(.+)_(.+)'

def get_num_nodes(filename: str) -> int:
    return int(re.match(RESULTS_FOLDER_REGEX, filename).group(1))

def get_btl(filename: str) -> float:
    return float(re.match(RESULTS_FOLDER_REGEX, filename).group(2))

def get_node_distribution(filename: str) -> str:
    return re.match(RESULTS_FOLDER_REGEX, filename).group(3)

def get_node_params(filename: str) -> List[float]:
    node_param_str = re.match(RESULTS_FOLDER_REGEX, filename).group(4)
    return list(map(lambda f_string: float(f_string.strip()), node_param_str[1:-1].split(",")))

def get_topology(filename: str) -> str:
    return re.match(RESULTS_FOLDER_REGEX, filename).group(5)

def get_topology_params(filename: str) -> List[int]:
    lst_str = re.match(RESULTS_FOLDER_REGEX, filename).group(6)[1:-1] 
    if lst_str == "":
        return []
    return list(map(lambda s: int(s.strip()), lst_str.split(",")))

def get_switch_distribution(filename: str) -> str:
    return re.match(RESULTS_FOLDER_REGEX, filename).group(7)

def get_switch_params(filename: str) -> List[float]:
    switch_param_str = re.match(RESULTS_FOLDER_REGEX, filename).group(8)
    return list(map(lambda f_string: float(f_string.strip()), switch_param_str[1:-1].split(",")))

def get_protocol(filename: str) -> str:
    return re.match(RESULTS_FOLDER_REGEX, filename).group(9)

def get_num_faults(filename: str) -> int:
    return int(re.match(RESULTS_FOLDER_REGEX, filename).group(10))

def get_fault_type(filename: str) -> str:
    return re.match(RESULTS_FOLDER_REGEX, filename).group(11)

def get_fault_params(filename: str) -> List[int]:
    lst_str = re.match(RESULTS_FOLDER_REGEX, filename).group(12)[1:-1] 
    if lst_str == "":
        return []
    return list(map(lambda s: int(s.strip()), lst_str.split(",")))

def construct_result_name_obj_from_filename(filename: str) -> ResultName:
    return ResultName().set_num_nodes(get_num_nodes(filename)).set_btl(get_btl(filename)) \
            .set_node_dist(get_node_distribution(filename)).set_node_params(get_node_params(filename)) \
            .set_topology(get_topology(filename)).set_topo_params(get_topology_params(filename)) \
            .set_switch_dist(get_switch_distribution(filename)).set_switch_params(get_switch_params(filename)) \
            .set_protocol(get_protocol(filename)) \
            .set_num_faults(get_num_faults(filename)).set_fault_type(get_fault_type(filename)).set_fault_params(get_fault_params(filename)) \

def construct_results_directory(num_nodes: int, base_time_limit: float, 
                                node_processing_distribution: str, node_processing_parameters: List[float],
                                topology: str, topo_params: List[int], switch_processing_distribution: str, switch_processing_parameters: List[float], 
                                protocol: str, num_faults: int, fault_type: str, fault_params: List[int]) -> str:
    return ResultName().set_num_nodes(num_nodes).set_btl(base_time_limit) \
            .set_node_dist(node_processing_distribution).set_node_params(node_processing_parameters) \
            .set_topology(topology).set_topo_params(topo_params) \
            .set_switch_dist(switch_processing_distribution).set_switch_params(switch_processing_parameters) \
            .set_protocol(protocol) \
            .set_num_faults(num_faults).set_fault_type(fault_type).set_fault_params(fault_params) \
            .build()

def create_run_config_json(num_runs: int, starting_seed: int, seed_multiplier: int, 
                           num_nodes: int, num_consensus: int, num_programs: int, base_time_limit: float, 
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
    validator_settings_dic = {"numNodes": num_nodes, "numConsensus": num_consensus, "numPrograms": num_programs, "baseTimeLimit": base_time_limit, 
                              "nodeProcessingDistribution": node_processing_distribution, 
                              "consensusProtocol": consensus_protocol, "faultSettings": fault_settings_dic}
    json_dic = {"numRuns": num_runs,"startingSeed": starting_seed, "seedMultiplier": seed_multiplier,
                "validatorSettings": validator_settings_dic, "networkSettings": network_settings_dic}
    return json.dumps(json_dic, indent=4)

def rename_results(folder: str):
    for dir in os.listdir(folder):
        if not dir.startswith("json"):
            continue

        # Each run has to be manually customised. Be careful. It cannot be undone.

        # result_obj = construct_result_name_obj_from_filename(dir)
        # topo = result_obj.get_topology()
        # if topo.lower() == "fc":
        #     topo_params = result_obj.get_topo_params()
        #     if len(topo_params) != 3:
        #         continue
        #     result_obj.set_topo_params([8, 4])
        #     print("before:", folder + dir, "after:", folder + result_obj.build())
        #     os.rename(folder + dir, folder + result_obj.build())
