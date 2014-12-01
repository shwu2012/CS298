import os
import os.path
import sys

def create_file_name(content, index):
  return str(index).rjust(4, '0') + '.txt'


def write_to_file(dir_path, content, index):
  if not os.path.exists(dir_path):
    os.mkdir(dir_path)
  file_path = os.path.join(dir_path, create_file_name(content, index))
  f_out = open(file_path, 'w')
  f_out.write(content)
  f_out.close()


source_file = sys.argv[1]
dest_dir = sys.argv[2]

f_in = open(source_file, 'r')
lines = f_in.readlines()
lines = [line for line in lines if line.strip() != '']
f_in.close()

index = 0
for line in lines:
  parts = line.split('\t')
  label = parts[0]
  content = parts[1]
  write_to_file(os.path.join(dest_dir, label), content, index)
  index += 1

