import SwiftUI
import ComposeApp

struct ServersView: View {
    @Environment(SettingsBridge.self) private var settingsBridge
    @State private var showAddServer = false

    var body: some View {
        List {
            if let settings = settingsBridge.settings {
                let servers: [Server] = settings.servers
                if servers.isEmpty {
                    Text("No servers configured")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(servers.indices, id: \.self) { index in
                        let server = servers[index]
                        VStack(alignment: .leading, spacing: 2) {
                            Text(server.name)
                                .font(.body)
                            Text(server.address)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(serverTypeLabel(server.type))
                                .font(.caption2)
                                .foregroundStyle(.tertiary)
                        }
                    }
                    .onDelete { indexSet in
                        for index in indexSet {
                            let server = servers[index]
                            settingsBridge.deleteServer(id: server.id.toHexDashString())
                        }
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle("Servers")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Add") {
                    showAddServer = true
                }
            }
            ToolbarItem(placement: .navigationBarLeading) {
                EditButton()
            }
        }
        .sheet(isPresented: $showAddServer) {
            AddServerSheet(isPresented: $showAddServer)
                .environment(settingsBridge)
        }
    }

    private func serverTypeLabel(_ type: ServerType) -> String {
        switch type {
        case .piaware:
            return "PiAware"
        case .readsb:
            return "readsb"
        default:
            return "Unknown"
        }
    }
}

// MARK: - Add Server Sheet

private struct AddServerSheet: View {
    @Environment(SettingsBridge.self) private var settingsBridge
    @Binding var isPresented: Bool

    @State private var name = ""
    @State private var address = ""
    @State private var selectedType: ServerType = .piaware

    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
            !address.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Server Details") {
                    TextField("Name", text: $name)
                        .autocorrectionDisabled()

                    TextField("Address (e.g. 192.168.1.100)", text: $address)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                }

                Section("Type") {
                    Picker("Server Type", selection: $selectedType) {
                        Text("PiAware").tag(ServerType.piaware)
                        Text("readsb").tag(ServerType.readsb)
                    }
                    .pickerStyle(.segmented)
                }
            }
            .navigationTitle("Add Server")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        settingsBridge.addServer(
                            name: name.trimmingCharacters(in: .whitespaces),
                            address: address.trimmingCharacters(in: .whitespaces),
                            type: selectedType
                        )
                        isPresented = false
                    }
                    .disabled(!isValid)
                }
            }
        }
    }
}
