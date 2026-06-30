import SwiftUI
import ComposeApp

struct ServersView: View {
    @Environment(SettingsBridge.self) private var settingsBridge
    @State private var showAddServer = false
    @State private var serverToEdit: Server? = nil

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
                        Button {
                            serverToEdit = server
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(server.name)
                                    .font(.body)
                                    .foregroundStyle(.primary)
                                Text(server.address)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(serverTypeLabel(server.type))
                                    .font(.caption2)
                                    .foregroundStyle(.tertiary)
                            }
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
                Button("Add") { showAddServer = true }
            }
            ToolbarItem(placement: .navigationBarLeading) {
                EditButton()
            }
        }
        .sheet(isPresented: $showAddServer) {
            AddServerSheet(isPresented: $showAddServer)
                .environment(settingsBridge)
        }
        .sheet(isPresented: Binding(
            get: { serverToEdit != nil },
            set: { if !$0 { serverToEdit = nil } }
        )) {
            if let server = serverToEdit {
                EditServerSheet(server: server, onDismiss: { serverToEdit = nil })
                    .environment(settingsBridge)
            }
        }
    }

    private func serverTypeLabel(_ type: ServerType) -> String {
        switch type {
        case .piaware: return "PiAware"
        case .readsb: return "readsb"
        default: return "Unknown"
        }
    }
}

// MARK: - Edit Server Sheet

private struct EditServerSheet: View {
    @Environment(SettingsBridge.self) private var settingsBridge
    let server: Server
    let onDismiss: () -> Void

    @State private var name: String
    @State private var address: String
    @State private var selectedType: ServerType

    init(server: Server, onDismiss: @escaping () -> Void) {
        self.server = server
        self.onDismiss = onDismiss
        self._name = State(initialValue: server.name)
        self._address = State(initialValue: server.address)
        self._selectedType = State(initialValue: server.type)
    }

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
            .navigationTitle("Edit Server")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        settingsBridge.editServer(
                            id: server.id.toHexDashString(),
                            name: name.trimmingCharacters(in: .whitespaces),
                            address: address.trimmingCharacters(in: .whitespaces),
                            type: selectedType
                        )
                        onDismiss()
                    }
                    .disabled(!isValid)
                }
            }
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
                    Button("Cancel") { isPresented = false }
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
