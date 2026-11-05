#!/usr/bin/env ruby
# Copyright 2026 Very Good Security Authors. Licensed under Apache-2.0.
require 'minitest/autorun'
require 'open3'
require 'yaml'

class WasmWorkerManifestTest < Minitest::Test
  def setup
    rendered, status = Open3.capture2e('kubectl', 'kustomize', __dir__)
    assert status.success?, rendered
    @objects = YAML.load_stream(rendered)
  end

  def object(kind)
    @objects.find { |entry| entry['kind'] == kind } || flunk("missing #{kind}")
  end

  def test_guest_and_keys_live_in_separate_limited_containers
    pod = object('Deployment').dig('spec', 'template', 'spec')
    assert_equal false, pod['automountServiceAccountToken']
    assert_equal false, pod['shareProcessNamespace']
    assert_equal false, pod['hostNetwork']
    assert_equal 'RuntimeDefault', pod.dig('securityContext', 'seccompProfile', 'type')
    containers = pod.fetch('containers').to_h { |entry| [entry.fetch('name'), entry] }
    assert_equal %w[gateway worker], containers.keys.sort
    containers.each_value do |container|
      security = container.fetch('securityContext')
      assert_equal true, security['runAsNonRoot']
      assert_equal false, security['allowPrivilegeEscalation']
      assert_equal true, security['readOnlyRootFilesystem']
      assert_equal ['ALL'], security.dig('capabilities', 'drop')
      assert container.dig('resources', 'limits', 'memory')
      assert container.dig('resources', 'limits', 'cpu')
    end
    secret_names = pod.fetch('volumes').select { |volume| volume.key?('secret') }
        .map { |volume| volume.fetch('name') }
    refute_empty secret_names
    gateway_mounts = containers.fetch('gateway').fetch('volumeMounts').map { |mount| mount['name'] }
    worker_mounts = containers.fetch('worker').fetch('volumeMounts').map { |mount| mount['name'] }
    assert (secret_names - gateway_mounts).empty?
    assert (secret_names & worker_mounts).empty?
  end

  def test_network_is_gateway_only_and_denies_egress
    policy = object('NetworkPolicy').fetch('spec')
    assert_equal %w[Egress Ingress], policy.fetch('policyTypes').sort
    assert_equal [], policy.fetch('egress')
    ingress = policy.fetch('ingress')
    assert_equal 1, ingress.length
    assert_equal 8443, ingress.first.fetch('ports').first.fetch('port')
    assert_equal 'atlas-horizon', ingress.first.fetch('from').first
        .dig('podSelector', 'matchLabels', 'app.kubernetes.io/name')
    service = object('Service').fetch('spec')
    assert_equal 8443, service.fetch('ports').first.fetch('targetPort')
  end
end
