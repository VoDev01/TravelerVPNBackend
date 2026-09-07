output "instances" {
  value = merge(
    { for idx, instance in module.nodes : instance.container_info.name => instance.container_info },
    { for idx, instance in module.panel : instance.container_info.name => instance.container_info },
    { for idx, instance in module.spring-backend : instance.container_info.name => instance.container_info }
  )
}