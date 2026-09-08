targetScope = 'resourceGroup'
param location string = resourceGroup().location
param prefix string = 'sussmartcare'
param aksDnsPrefix string = '${prefix}-aks'

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: replace('${prefix}acr','-','')
  location: location
  sku: { name: 'Basic' }
  properties: { adminUserEnabled: false }
}

resource log 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${prefix}-log'
  location: location
  properties: { retentionInDays: 30 }
}

resource aks 'Microsoft.ContainerService/managedClusters@2024-05-01' = {
  name: '${prefix}-aks'
  location: location
  identity: { type: 'SystemAssigned' }
  properties: {
    dnsPrefix: aksDnsPrefix
    agentPoolProfiles: [
      { name: 'system', count: 2, vmSize: 'Standard_B2s', mode: 'System', osType: 'Linux', type: 'VirtualMachineScaleSets', enableAutoScaling: true, minCount: 2, maxCount: 5 }
    ]
    networkProfile: { networkPlugin: 'azure', loadBalancerSku: 'standard' }
    addonProfiles: { omsagent: { enabled: true, config: { logAnalyticsWorkspaceResourceID: log.id } } }
  }
}

resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(aks.id, acr.id, 'AcrPull')
  scope: acr
  properties: { roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions','7f951dda-4ed3-4680-a7ca-43fe172d538d'), principalId: aks.properties.identityProfile.kubeletidentity.objectId, principalType: 'ServicePrincipal' }
}
output aksName string = aks.name
output acrLoginServer string = acr.properties.loginServer
