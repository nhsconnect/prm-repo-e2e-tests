import os
import json

def load_file(filename):
  with open(filename, 'r') as f:
    return f.read()

def write_to_file(filename, content):
  with open(filename, 'w') as f:
    f.write(content)


def make_dir(dirname):
  os.makedirs(dirname, exist_ok=True)

os.chdir('samples/')
core_message_id_to_repack = 'ae6f8d45-913a-4c67-89f3-10d131fc332c'

id = core_message_id_to_repack
mhs_json_out_dir = 'mhs-json-out'
make_dir(mhs_json_out_dir)
target_filename = mhs_json_out_dir + '/' + id

ebXml = load_file(id + '/ebxml.xml')
payload = load_file(id + '/payload.xml')
mhs_json_data = {
  'ebXML': ebXml,
  'payload': payload,
  'attachments': []
}

write_to_file(target_filename, json.dumps(mhs_json_data))

