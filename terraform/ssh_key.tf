resource "serverspace_ssh" "node_key" {
  name = "terraform-key"
  public_key = file("./vless_node.pub")
}